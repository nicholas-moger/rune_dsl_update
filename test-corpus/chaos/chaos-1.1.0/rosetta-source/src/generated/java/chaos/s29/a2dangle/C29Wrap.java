package chaos.s29.a2dangle;

import chaos.s29.a2dangle.h.C29Leaf;
import chaos.s29.a2dangle.meta.C29WrapMeta;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Outer option 2 - the same attributes.
 * @version 1.0.0
 */
@RosettaDataType(value="C29Wrap", builder=C29Wrap.C29WrapBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C29Wrap", model="chaos", builder=C29Wrap.C29WrapBuilderImpl.class, version="1.0.0")
public interface C29Wrap extends RosettaModelObject {

	C29WrapMeta metaData = new C29WrapMeta();

	/*********************** Getter Methods  ***********************/
	String getText();
	List<? extends C29Leaf> getLeaves();
	FieldWithMetaString getCode();
	List<? extends FieldWithMetaString> getCodes();
	List<? extends C29Inner> getInners();

	/*********************** Build Methods  ***********************/
	C29Wrap build();
	
	C29Wrap.C29WrapBuilder toBuilder();
	
	static C29Wrap.C29WrapBuilder builder() {
		return new C29Wrap.C29WrapBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C29Wrap> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C29Wrap> getType() {
		return C29Wrap.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
		processRosetta(path.newSubPath("leaves"), processor, C29Leaf.class, getLeaves());
		processRosetta(path.newSubPath("code"), processor, FieldWithMetaString.class, getCode());
		processRosetta(path.newSubPath("codes"), processor, FieldWithMetaString.class, getCodes());
		processRosetta(path.newSubPath("inners"), processor, C29Inner.class, getInners());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C29WrapBuilder extends C29Wrap, RosettaModelObjectBuilder {
		C29Leaf.C29LeafBuilder getOrCreateLeaves(int index);
		@Override
		List<? extends C29Leaf.C29LeafBuilder> getLeaves();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCode();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getCode();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCodes(int index);
		@Override
		List<? extends FieldWithMetaString.FieldWithMetaStringBuilder> getCodes();
		C29Inner.C29InnerBuilder getOrCreateInners(int index);
		@Override
		List<? extends C29Inner.C29InnerBuilder> getInners();
		C29Wrap.C29WrapBuilder setText(String text);
		C29Wrap.C29WrapBuilder addLeaves(C29Leaf leaves);
		C29Wrap.C29WrapBuilder addLeaves(C29Leaf leaves, int idx);
		C29Wrap.C29WrapBuilder addLeaves(List<? extends C29Leaf> leaves);
		C29Wrap.C29WrapBuilder setLeaves(List<? extends C29Leaf> leaves);
		C29Wrap.C29WrapBuilder setCode(FieldWithMetaString code);
		C29Wrap.C29WrapBuilder setCodeValue(String code);
		C29Wrap.C29WrapBuilder addCodes(FieldWithMetaString codes);
		C29Wrap.C29WrapBuilder addCodes(FieldWithMetaString codes, int idx);
		C29Wrap.C29WrapBuilder addCodesValue(String codes);
		C29Wrap.C29WrapBuilder addCodesValue(String codes, int idx);
		C29Wrap.C29WrapBuilder addCodes(List<? extends FieldWithMetaString> codes);
		C29Wrap.C29WrapBuilder setCodes(List<? extends FieldWithMetaString> codes);
		C29Wrap.C29WrapBuilder addCodesValue(List<? extends String> codes);
		C29Wrap.C29WrapBuilder setCodesValue(List<? extends String> codes);
		C29Wrap.C29WrapBuilder addInners(C29Inner inners);
		C29Wrap.C29WrapBuilder addInners(C29Inner inners, int idx);
		C29Wrap.C29WrapBuilder addInners(List<? extends C29Inner> inners);
		C29Wrap.C29WrapBuilder setInners(List<? extends C29Inner> inners);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
			processRosetta(path.newSubPath("leaves"), processor, C29Leaf.C29LeafBuilder.class, getLeaves());
			processRosetta(path.newSubPath("code"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCode());
			processRosetta(path.newSubPath("codes"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCodes());
			processRosetta(path.newSubPath("inners"), processor, C29Inner.C29InnerBuilder.class, getInners());
		}
		

		C29Wrap.C29WrapBuilder prune();
	}

	/*********************** Immutable Implementation of C29Wrap  ***********************/
	class C29WrapImpl implements C29Wrap {
		private final String text;
		private final List<? extends C29Leaf> leaves;
		private final FieldWithMetaString code;
		private final List<? extends FieldWithMetaString> codes;
		private final List<? extends C29Inner> inners;
		
		protected C29WrapImpl(C29Wrap.C29WrapBuilder builder) {
			this.text = builder.getText();
			this.leaves = ofNullable(builder.getLeaves()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.code = ofNullable(builder.getCode()).map(f->f.build()).orElse(null);
			this.codes = ofNullable(builder.getCodes()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.inners = ofNullable(builder.getInners()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("text")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("text")
		public String getText() {
			return text;
		}
		
		@Override
		@RosettaAttribute("leaves")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("leaves")
		public List<? extends C29Leaf> getLeaves() {
			return leaves;
		}
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("code")
		public FieldWithMetaString getCode() {
			return code;
		}
		
		@Override
		@RosettaAttribute("codes")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("codes")
		public List<? extends FieldWithMetaString> getCodes() {
			return codes;
		}
		
		@Override
		@RosettaAttribute("inners")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("inners")
		public List<? extends C29Inner> getInners() {
			return inners;
		}
		
		@Override
		public C29Wrap build() {
			return this;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder toBuilder() {
			C29Wrap.C29WrapBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C29Wrap.C29WrapBuilder builder) {
			ofNullable(getText()).ifPresent(builder::setText);
			ofNullable(getLeaves()).ifPresent(builder::setLeaves);
			ofNullable(getCode()).ifPresent(builder::setCode);
			ofNullable(getCodes()).ifPresent(builder::setCodes);
			ofNullable(getInners()).ifPresent(builder::setInners);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29Wrap _that = getType().cast(o);
		
			if (!Objects.equals(text, _that.getText())) return false;
			if (!ListEquals.listEquals(leaves, _that.getLeaves())) return false;
			if (!Objects.equals(code, _that.getCode())) return false;
			if (!ListEquals.listEquals(codes, _that.getCodes())) return false;
			if (!ListEquals.listEquals(inners, _that.getInners())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (text != null ? text.hashCode() : 0);
			_result = 31 * _result + (leaves != null ? leaves.hashCode() : 0);
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			_result = 31 * _result + (codes != null ? codes.hashCode() : 0);
			_result = 31 * _result + (inners != null ? inners.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C29Wrap {" +
				"text=" + this.text + ", " +
				"leaves=" + this.leaves + ", " +
				"code=" + this.code + ", " +
				"codes=" + this.codes + ", " +
				"inners=" + this.inners +
			'}';
		}
	}

	/*********************** Builder Implementation of C29Wrap  ***********************/
	class C29WrapBuilderImpl implements C29Wrap.C29WrapBuilder {
	
		protected String text;
		protected List<C29Leaf.C29LeafBuilder> leaves = new ArrayList<>();
		protected FieldWithMetaString.FieldWithMetaStringBuilder code;
		protected List<FieldWithMetaString.FieldWithMetaStringBuilder> codes = new ArrayList<>();
		protected List<C29Inner.C29InnerBuilder> inners = new ArrayList<>();
		
		@Override
		@RosettaAttribute("text")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("text")
		public String getText() {
			return text;
		}
		
		@Override
		@RosettaAttribute("leaves")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("leaves")
		public List<? extends C29Leaf.C29LeafBuilder> getLeaves() {
			return leaves;
		}
		
		@Override
		public C29Leaf.C29LeafBuilder getOrCreateLeaves(int index) {
			if (leaves==null) {
				this.leaves = new ArrayList<>();
			}
			return getIndex(leaves, index, () -> {
						C29Leaf.C29LeafBuilder newLeaves = C29Leaf.builder();
						return newLeaves;
					});
		}
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("code")
		public FieldWithMetaString.FieldWithMetaStringBuilder getCode() {
			return code;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCode() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (code!=null) {
				result = code;
			}
			else {
				result = code = FieldWithMetaString.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("codes")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("codes")
		public List<? extends FieldWithMetaString.FieldWithMetaStringBuilder> getCodes() {
			return codes;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCodes(int index) {
			if (codes==null) {
				this.codes = new ArrayList<>();
			}
			return getIndex(codes, index, () -> {
						FieldWithMetaString.FieldWithMetaStringBuilder newCodes = FieldWithMetaString.builder();
						return newCodes;
					});
		}
		
		@Override
		@RosettaAttribute("inners")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("inners")
		public List<? extends C29Inner.C29InnerBuilder> getInners() {
			return inners;
		}
		
		@Override
		public C29Inner.C29InnerBuilder getOrCreateInners(int index) {
			if (inners==null) {
				this.inners = new ArrayList<>();
			}
			return getIndex(inners, index, () -> {
						C29Inner.C29InnerBuilder newInners = C29Inner.builder();
						return newInners;
					});
		}
		
		@RosettaAttribute("text")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("text")
		@Override
		public C29Wrap.C29WrapBuilder setText(String _text) {
			this.text = _text == null ? null : _text;
			return this;
		}
		
		@RosettaAttribute("leaves")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("leaves")
		@Override
		public C29Wrap.C29WrapBuilder addLeaves(C29Leaf _leaves) {
			if (_leaves != null) {
				this.leaves.add(_leaves.toBuilder());
			}
			return this;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder addLeaves(C29Leaf _leaves, int idx) {
			getIndex(this.leaves, idx, () -> _leaves.toBuilder());
			return this;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder addLeaves(List<? extends C29Leaf> leavess) {
			if (leavess != null) {
				for (final C29Leaf toAdd : leavess) {
					this.leaves.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("leaves")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("leaves")
		@Override
		public C29Wrap.C29WrapBuilder setLeaves(List<? extends C29Leaf> leavess) {
			if (leavess == null) {
				this.leaves = new ArrayList<>();
			} else {
				this.leaves = leavess.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("code")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("code")
		@Override
		public C29Wrap.C29WrapBuilder setCode(FieldWithMetaString _code) {
			this.code = _code == null ? null : _code.toBuilder();
			return this;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder setCodeValue(String _code) {
			this.getOrCreateCode().setValue(_code);
			return this;
		}
		
		@RosettaAttribute("codes")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("codes")
		@Override
		public C29Wrap.C29WrapBuilder addCodes(FieldWithMetaString _codes) {
			if (_codes != null) {
				this.codes.add(_codes.toBuilder());
			}
			return this;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder addCodes(FieldWithMetaString _codes, int idx) {
			getIndex(this.codes, idx, () -> _codes.toBuilder());
			return this;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder addCodesValue(String _codes) {
			this.getOrCreateCodes(-1).setValue(_codes);
			return this;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder addCodesValue(String _codes, int idx) {
			this.getOrCreateCodes(idx).setValue(_codes);
			return this;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder addCodes(List<? extends FieldWithMetaString> codess) {
			if (codess != null) {
				for (final FieldWithMetaString toAdd : codess) {
					this.codes.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("codes")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("codes")
		@Override
		public C29Wrap.C29WrapBuilder setCodes(List<? extends FieldWithMetaString> codess) {
			if (codess == null) {
				this.codes = new ArrayList<>();
			} else {
				this.codes = codess.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder addCodesValue(List<? extends String> codess) {
			if (codess != null) {
				for (final String toAdd : codess) {
					this.addCodesValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder setCodesValue(List<? extends String> codess) {
			this.codes.clear();
			if (codess != null) {
				codess.forEach(this::addCodesValue);
			}
			return this;
		}
		
		@RosettaAttribute("inners")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("inners")
		@Override
		public C29Wrap.C29WrapBuilder addInners(C29Inner _inners) {
			if (_inners != null) {
				this.inners.add(_inners.toBuilder());
			}
			return this;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder addInners(C29Inner _inners, int idx) {
			getIndex(this.inners, idx, () -> _inners.toBuilder());
			return this;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder addInners(List<? extends C29Inner> innerss) {
			if (innerss != null) {
				for (final C29Inner toAdd : innerss) {
					this.inners.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("inners")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("inners")
		@Override
		public C29Wrap.C29WrapBuilder setInners(List<? extends C29Inner> innerss) {
			if (innerss == null) {
				this.inners = new ArrayList<>();
			} else {
				this.inners = innerss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public C29Wrap build() {
			return new C29Wrap.C29WrapImpl(this);
		}
		
		@Override
		public C29Wrap.C29WrapBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29Wrap.C29WrapBuilder prune() {
			leaves = leaves.stream().filter(b->b!=null).<C29Leaf.C29LeafBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			if (code!=null && !code.prune().hasData()) code = null;
			codes = codes.stream().filter(b->b!=null).<FieldWithMetaString.FieldWithMetaStringBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			inners = inners.stream().filter(b->b!=null).<C29Inner.C29InnerBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getText()!=null) return true;
			if (getLeaves()!=null && getLeaves().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			if (getCode()!=null) return true;
			if (getCodes()!=null && !getCodes().isEmpty()) return true;
			if (getInners()!=null && getInners().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29Wrap.C29WrapBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C29Wrap.C29WrapBuilder o = (C29Wrap.C29WrapBuilder) other;
			
			merger.mergeRosetta(getLeaves(), o.getLeaves(), this::getOrCreateLeaves);
			merger.mergeRosetta(getCode(), o.getCode(), this::setCode);
			merger.mergeRosetta(getCodes(), o.getCodes(), this::getOrCreateCodes);
			merger.mergeRosetta(getInners(), o.getInners(), this::getOrCreateInners);
			
			merger.mergeBasic(getText(), o.getText(), this::setText);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29Wrap _that = getType().cast(o);
		
			if (!Objects.equals(text, _that.getText())) return false;
			if (!ListEquals.listEquals(leaves, _that.getLeaves())) return false;
			if (!Objects.equals(code, _that.getCode())) return false;
			if (!ListEquals.listEquals(codes, _that.getCodes())) return false;
			if (!ListEquals.listEquals(inners, _that.getInners())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (text != null ? text.hashCode() : 0);
			_result = 31 * _result + (leaves != null ? leaves.hashCode() : 0);
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			_result = 31 * _result + (codes != null ? codes.hashCode() : 0);
			_result = 31 * _result + (inners != null ? inners.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C29WrapBuilder {" +
				"text=" + this.text + ", " +
				"leaves=" + this.leaves + ", " +
				"code=" + this.code + ", " +
				"codes=" + this.codes + ", " +
				"inners=" + this.inners +
			'}';
		}
	}
}
