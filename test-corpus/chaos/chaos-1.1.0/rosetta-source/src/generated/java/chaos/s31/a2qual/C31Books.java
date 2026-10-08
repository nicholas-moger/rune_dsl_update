package chaos.s31.a2qual;

import chaos.s31.a2qual.h.C31Held;
import chaos.s31.a2qual.meta.C31BooksMeta;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Both colliding enums at attribute seats, a scheme-carrying scalar, an enum list and the top of the extends chain.
 * @version 1.0.0
 */
@RosettaDataType(value="C31Books", builder=C31Books.C31BooksBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C31Books", model="chaos", builder=C31Books.C31BooksBuilderImpl.class, version="1.0.0")
public interface C31Books extends RosettaModelObject {

	C31BooksMeta metaData = new C31BooksMeta();

	/*********************** Getter Methods  ***********************/
	C31SideEnum getSide();
	C31MoveEnum getMove();
	FieldWithMetaString getCoded();
	List<C31SideEnum> getSides();
	C31TopEnum getTop();
	C31Held getHeld();

	/*********************** Build Methods  ***********************/
	C31Books build();
	
	C31Books.C31BooksBuilder toBuilder();
	
	static C31Books.C31BooksBuilder builder() {
		return new C31Books.C31BooksBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C31Books> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C31Books> getType() {
		return C31Books.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("side"), C31SideEnum.class, getSide(), this);
		processor.processBasic(path.newSubPath("move"), C31MoveEnum.class, getMove(), this);
		processRosetta(path.newSubPath("coded"), processor, FieldWithMetaString.class, getCoded());
		processor.processBasic(path.newSubPath("sides"), C31SideEnum.class, getSides(), this);
		processor.processBasic(path.newSubPath("top"), C31TopEnum.class, getTop(), this);
		processRosetta(path.newSubPath("held"), processor, C31Held.class, getHeld());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C31BooksBuilder extends C31Books, RosettaModelObjectBuilder {
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCoded();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getCoded();
		C31Held.C31HeldBuilder getOrCreateHeld();
		@Override
		C31Held.C31HeldBuilder getHeld();
		C31Books.C31BooksBuilder setSide(C31SideEnum side);
		C31Books.C31BooksBuilder setMove(C31MoveEnum move);
		C31Books.C31BooksBuilder setCoded(FieldWithMetaString coded);
		C31Books.C31BooksBuilder setCodedValue(String coded);
		C31Books.C31BooksBuilder addSides(C31SideEnum sides);
		C31Books.C31BooksBuilder addSides(C31SideEnum sides, int idx);
		C31Books.C31BooksBuilder addSides(List<C31SideEnum> sides);
		C31Books.C31BooksBuilder setSides(List<C31SideEnum> sides);
		C31Books.C31BooksBuilder setTop(C31TopEnum top);
		C31Books.C31BooksBuilder setHeld(C31Held held);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("side"), C31SideEnum.class, getSide(), this);
			processor.processBasic(path.newSubPath("move"), C31MoveEnum.class, getMove(), this);
			processRosetta(path.newSubPath("coded"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCoded());
			processor.processBasic(path.newSubPath("sides"), C31SideEnum.class, getSides(), this);
			processor.processBasic(path.newSubPath("top"), C31TopEnum.class, getTop(), this);
			processRosetta(path.newSubPath("held"), processor, C31Held.C31HeldBuilder.class, getHeld());
		}
		

		C31Books.C31BooksBuilder prune();
	}

	/*********************** Immutable Implementation of C31Books  ***********************/
	class C31BooksImpl implements C31Books {
		private final C31SideEnum side;
		private final C31MoveEnum move;
		private final FieldWithMetaString coded;
		private final List<C31SideEnum> sides;
		private final C31TopEnum top;
		private final C31Held held;
		
		protected C31BooksImpl(C31Books.C31BooksBuilder builder) {
			this.side = builder.getSide();
			this.move = builder.getMove();
			this.coded = ofNullable(builder.getCoded()).map(f->f.build()).orElse(null);
			this.sides = ofNullable(builder.getSides()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.top = builder.getTop();
			this.held = ofNullable(builder.getHeld()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("side")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("side")
		public C31SideEnum getSide() {
			return side;
		}
		
		@Override
		@RosettaAttribute("move")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("move")
		public C31MoveEnum getMove() {
			return move;
		}
		
		@Override
		@RosettaAttribute("coded")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("coded")
		public FieldWithMetaString getCoded() {
			return coded;
		}
		
		@Override
		@RosettaAttribute("sides")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("sides")
		public List<C31SideEnum> getSides() {
			return sides;
		}
		
		@Override
		@RosettaAttribute("top")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("top")
		public C31TopEnum getTop() {
			return top;
		}
		
		@Override
		@RosettaAttribute("held")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("held")
		public C31Held getHeld() {
			return held;
		}
		
		@Override
		public C31Books build() {
			return this;
		}
		
		@Override
		public C31Books.C31BooksBuilder toBuilder() {
			C31Books.C31BooksBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C31Books.C31BooksBuilder builder) {
			ofNullable(getSide()).ifPresent(builder::setSide);
			ofNullable(getMove()).ifPresent(builder::setMove);
			ofNullable(getCoded()).ifPresent(builder::setCoded);
			ofNullable(getSides()).ifPresent(builder::setSides);
			ofNullable(getTop()).ifPresent(builder::setTop);
			ofNullable(getHeld()).ifPresent(builder::setHeld);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C31Books _that = getType().cast(o);
		
			if (!Objects.equals(side, _that.getSide())) return false;
			if (!Objects.equals(move, _that.getMove())) return false;
			if (!Objects.equals(coded, _that.getCoded())) return false;
			if (!ListEquals.listEquals(sides, _that.getSides())) return false;
			if (!Objects.equals(top, _that.getTop())) return false;
			if (!Objects.equals(held, _that.getHeld())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (side != null ? side.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (move != null ? move.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (coded != null ? coded.hashCode() : 0);
			_result = 31 * _result + (sides != null ? sides.stream().map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);
			_result = 31 * _result + (top != null ? top.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (held != null ? held.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C31Books {" +
				"side=" + this.side + ", " +
				"move=" + this.move + ", " +
				"coded=" + this.coded + ", " +
				"sides=" + this.sides + ", " +
				"top=" + this.top + ", " +
				"held=" + this.held +
			'}';
		}
	}

	/*********************** Builder Implementation of C31Books  ***********************/
	class C31BooksBuilderImpl implements C31Books.C31BooksBuilder {
	
		protected C31SideEnum side;
		protected C31MoveEnum move;
		protected FieldWithMetaString.FieldWithMetaStringBuilder coded;
		protected List<C31SideEnum> sides = new ArrayList<>();
		protected C31TopEnum top;
		protected C31Held.C31HeldBuilder held;
		
		@Override
		@RosettaAttribute("side")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("side")
		public C31SideEnum getSide() {
			return side;
		}
		
		@Override
		@RosettaAttribute("move")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("move")
		public C31MoveEnum getMove() {
			return move;
		}
		
		@Override
		@RosettaAttribute("coded")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("coded")
		public FieldWithMetaString.FieldWithMetaStringBuilder getCoded() {
			return coded;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCoded() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (coded!=null) {
				result = coded;
			}
			else {
				result = coded = FieldWithMetaString.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("sides")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("sides")
		public List<C31SideEnum> getSides() {
			return sides;
		}
		
		@Override
		@RosettaAttribute("top")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("top")
		public C31TopEnum getTop() {
			return top;
		}
		
		@Override
		@RosettaAttribute("held")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("held")
		public C31Held.C31HeldBuilder getHeld() {
			return held;
		}
		
		@Override
		public C31Held.C31HeldBuilder getOrCreateHeld() {
			C31Held.C31HeldBuilder result;
			if (held!=null) {
				result = held;
			}
			else {
				result = held = C31Held.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("side")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("side")
		@Override
		public C31Books.C31BooksBuilder setSide(C31SideEnum _side) {
			this.side = _side == null ? null : _side;
			return this;
		}
		
		@RosettaAttribute("move")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("move")
		@Override
		public C31Books.C31BooksBuilder setMove(C31MoveEnum _move) {
			this.move = _move == null ? null : _move;
			return this;
		}
		
		@RosettaAttribute("coded")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("coded")
		@Override
		public C31Books.C31BooksBuilder setCoded(FieldWithMetaString _coded) {
			this.coded = _coded == null ? null : _coded.toBuilder();
			return this;
		}
		
		@Override
		public C31Books.C31BooksBuilder setCodedValue(String _coded) {
			this.getOrCreateCoded().setValue(_coded);
			return this;
		}
		
		@RosettaAttribute("sides")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("sides")
		@Override
		public C31Books.C31BooksBuilder addSides(C31SideEnum _sides) {
			if (_sides != null) {
				this.sides.add(_sides);
			}
			return this;
		}
		
		@Override
		public C31Books.C31BooksBuilder addSides(C31SideEnum _sides, int idx) {
			getIndex(this.sides, idx, () -> _sides);
			return this;
		}
		
		@Override
		public C31Books.C31BooksBuilder addSides(List<C31SideEnum> sidess) {
			if (sidess != null) {
				for (final C31SideEnum toAdd : sidess) {
					this.sides.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("sides")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("sides")
		@Override
		public C31Books.C31BooksBuilder setSides(List<C31SideEnum> sidess) {
			if (sidess == null) {
				this.sides = new ArrayList<>();
			} else {
				this.sides = sidess.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("top")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("top")
		@Override
		public C31Books.C31BooksBuilder setTop(C31TopEnum _top) {
			this.top = _top == null ? null : _top;
			return this;
		}
		
		@RosettaAttribute("held")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("held")
		@Override
		public C31Books.C31BooksBuilder setHeld(C31Held _held) {
			this.held = _held == null ? null : _held.toBuilder();
			return this;
		}
		
		@Override
		public C31Books build() {
			return new C31Books.C31BooksImpl(this);
		}
		
		@Override
		public C31Books.C31BooksBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C31Books.C31BooksBuilder prune() {
			if (coded!=null && !coded.prune().hasData()) coded = null;
			if (held!=null && !held.prune().hasData()) held = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getSide()!=null) return true;
			if (getMove()!=null) return true;
			if (getCoded()!=null) return true;
			if (getSides()!=null && !getSides().isEmpty()) return true;
			if (getTop()!=null) return true;
			if (getHeld()!=null && getHeld().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C31Books.C31BooksBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C31Books.C31BooksBuilder o = (C31Books.C31BooksBuilder) other;
			
			merger.mergeRosetta(getCoded(), o.getCoded(), this::setCoded);
			merger.mergeRosetta(getHeld(), o.getHeld(), this::setHeld);
			
			merger.mergeBasic(getSide(), o.getSide(), this::setSide);
			merger.mergeBasic(getMove(), o.getMove(), this::setMove);
			merger.mergeBasic(getSides(), o.getSides(), (Consumer<C31SideEnum>) this::addSides);
			merger.mergeBasic(getTop(), o.getTop(), this::setTop);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C31Books _that = getType().cast(o);
		
			if (!Objects.equals(side, _that.getSide())) return false;
			if (!Objects.equals(move, _that.getMove())) return false;
			if (!Objects.equals(coded, _that.getCoded())) return false;
			if (!ListEquals.listEquals(sides, _that.getSides())) return false;
			if (!Objects.equals(top, _that.getTop())) return false;
			if (!Objects.equals(held, _that.getHeld())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (side != null ? side.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (move != null ? move.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (coded != null ? coded.hashCode() : 0);
			_result = 31 * _result + (sides != null ? sides.stream().map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);
			_result = 31 * _result + (top != null ? top.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (held != null ? held.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C31BooksBuilder {" +
				"side=" + this.side + ", " +
				"move=" + this.move + ", " +
				"coded=" + this.coded + ", " +
				"sides=" + this.sides + ", " +
				"top=" + this.top + ", " +
				"held=" + this.held +
			'}';
		}
	}
}
