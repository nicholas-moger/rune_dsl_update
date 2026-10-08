package chaos.s16.x36type.p1;

import chaos.s16.x36type.p1.meta.C16AlphaMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Choice option 1 - its NAME is the shadowing battery&#39;s subject.
 * @version 1.0.0
 */
@RosettaDataType(value="C16Alpha", builder=C16Alpha.C16AlphaBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C16Alpha", model="chaos", builder=C16Alpha.C16AlphaBuilderImpl.class, version="1.0.0")
public interface C16Alpha extends RosettaModelObject {

	C16AlphaMeta metaData = new C16AlphaMeta();

	/*********************** Getter Methods  ***********************/
	String getA();

	/*********************** Build Methods  ***********************/
	C16Alpha build();
	
	C16Alpha.C16AlphaBuilder toBuilder();
	
	static C16Alpha.C16AlphaBuilder builder() {
		return new C16Alpha.C16AlphaBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C16Alpha> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C16Alpha> getType() {
		return C16Alpha.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("a"), String.class, getA(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C16AlphaBuilder extends C16Alpha, RosettaModelObjectBuilder {
		C16Alpha.C16AlphaBuilder setA(String a);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("a"), String.class, getA(), this);
		}
		

		C16Alpha.C16AlphaBuilder prune();
	}

	/*********************** Immutable Implementation of C16Alpha  ***********************/
	class C16AlphaImpl implements C16Alpha {
		private final String a;
		
		protected C16AlphaImpl(C16Alpha.C16AlphaBuilder builder) {
			this.a = builder.getA();
		}
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("a")
		public String getA() {
			return a;
		}
		
		@Override
		public C16Alpha build() {
			return this;
		}
		
		@Override
		public C16Alpha.C16AlphaBuilder toBuilder() {
			C16Alpha.C16AlphaBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C16Alpha.C16AlphaBuilder builder) {
			ofNullable(getA()).ifPresent(builder::setA);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Alpha _that = getType().cast(o);
		
			if (!Objects.equals(a, _that.getA())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (a != null ? a.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C16Alpha {" +
				"a=" + this.a +
			'}';
		}
	}

	/*********************** Builder Implementation of C16Alpha  ***********************/
	class C16AlphaBuilderImpl implements C16Alpha.C16AlphaBuilder {
	
		protected String a;
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("a")
		public String getA() {
			return a;
		}
		
		@RosettaAttribute("a")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("a")
		@Override
		public C16Alpha.C16AlphaBuilder setA(String _a) {
			this.a = _a == null ? null : _a;
			return this;
		}
		
		@Override
		public C16Alpha build() {
			return new C16Alpha.C16AlphaImpl(this);
		}
		
		@Override
		public C16Alpha.C16AlphaBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Alpha.C16AlphaBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getA()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Alpha.C16AlphaBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C16Alpha.C16AlphaBuilder o = (C16Alpha.C16AlphaBuilder) other;
			
			
			merger.mergeBasic(getA(), o.getA(), this::setA);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Alpha _that = getType().cast(o);
		
			if (!Objects.equals(a, _that.getA())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (a != null ? a.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C16AlphaBuilder {" +
				"a=" + this.a +
			'}';
		}
	}
}
